;(function (root, $, mustache) {

  if (typeof root.eclimHttp === "undefined") {
    root.eclimHttp = {};
  }

  var ns = {
    messageFilter: "",
    pathFilter: "",
    APIURL: "/api/problems/-p",
    TMPL: $("#row-template").html(),
    REFRESH_DELAY: 1000, // milliseconds
    SORT_ORDER: ["warning", "filename", "message", "line", "column"]
  },
  projectSelector = root.eclimHttp.projectSelector;

  ns.shrinkPath = function (path) {
    var parts = path.split("/"), output = "";
    $.each(parts, function (i, part) {
      if (part[0]) {
        if (i !== parts.length - 1) {
          output += part[0];
          output += "/";
        } else {
          output += part;
        }
      }
    });
    return output;
  };

  /**
   * Sorts by order: warning, filename, message, line, column
   */
  ns.sortPredicate = function (rowA, rowB) {
    var i, key;
    for (i = 0; i < ns.SORT_ORDER.length; i++) {
      key = ns.SORT_ORDER[i];
      if (rowA[key] !== rowB[key]) {
        return rowA[key] > rowB[key] ? 1 : -1;
      }
    }
    return 0;
  };

  ns.render = function () {
    console.info("Rendering problems.");
    if (ns.data) {
      var html = "";
      $.each(ns.data, function (i, row) {

        if (ns.messageFilter.length > 0) {
          var search = ns.messageFilter.toLowerCase();
          var message = row.message;
          if (message) {
            message = message.toLowerCase();
            if (message.indexOf(search) === -1) {
              return;
            }
          }
        }

        if (ns.pathFilter.length > 0) {
          var search = ns.pathFilter.toLowerCase();
          var filename = row.filename;
          if (filename) {
            filename = filename.toLowerCase();
            if (filename.indexOf(search) === -1) {
              return;
            }
          }
        }

        row["filenameShort"] = ns.shrinkPath(row["filename"]);
        html += mustache.render(ns.TMPL, row);
      });
      $("#report-body").html(html);
    }
  };

  ns.getProblems = function (projectName) {
    $.getJSON(ns.APIURL + "/" + projectName, function (data) {
      ns.data = data;
      ns.data.sort(ns.sortPredicate);
      $(ns).trigger("problems:updated");
    });
  };

  $(ns).on("problems:updated", ns.render);

  $(function () {
    var selector = new projectSelector.ProjectSelector("project-name"),
        intervalId = -1;

    $(selector).on("project-selector:change", function () {
      clearInterval(intervalId);
      if (selector.selectedProject.name !== "Select One") {
        intervalId = setInterval(function () {
          ns.getProblems(selector.selectedProject.name);
        }, ns.REFRESH_DELAY);
      }
    });

    selector.getProjects();

    $("#report-header").bind("keyup blur change", function () {
      ns.messageFilter = $("#message-filter").val();
      ns.pathFilter = $("#path-filter").val();
      $(ns).trigger("problems:updated");
    });

    $("#report-body").on("mouseenter", "td.filename", function () {
      $(this).text($(this).data("full"));
    }).on("mouseleave", "td.filename", function () {
      $(this).text($(this).data("short"));
    });

  });

  root.eclimHttp.problems = ns;
}(this, jQuery, Mustache));
