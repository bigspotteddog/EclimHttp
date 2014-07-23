/*
 This file must be copy-pasted into problems.html for the moment. It's
 maintained externally in order to facilitate working on it. -WKM
*/

(function ($, mustache) {

  var ns = {
    messageFilter: "",
    pathFilter: "",
    APIURL: window.location.href.replace("/data/", "/api/"),
    TMPL: $("#row-template").html(),
    REFRESH_DELAY: 1000 // milliseconds
  };

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
   * Sort by filename, then by message, then by line number, then by column
   */
  ns.sortPredicate = function (rowA, rowB) {
    if (rowA.filename === rowB.filename) {
      if (rowA.message === rowB.message) {
        if (rowA.line === rowB.line) {
          if (rowA.column === rowB.column) {
            return 0;
          } else {
            return rowA.column > rowB.column ? -1 : 1;
          }
        } else {
          return rowA.line > rowB.line ? -1 : 1;
        }
      } else {
        return rowA.line > rowB.line ? -1 : 1;
      }
    } else {
      return rowA.filename > rowB.filename ? -1 : 1;
    }
  };

  ns.render = function () {
    console.info("Rendering problems.");
    if (ns.data) {
      var html = "";
      $.each(ns.data, function (i, row) {

        if (ns.messageFilter.length > 0) {
          if (row.message.indexOf(ns.messageFilter) === -1) {
            return;
          }
        }

        if (ns.pathFilter.length > 0) {
          if (row.path.indexOf(ns.pathFilter) === -1) {
            return;
          }
        }

        row["filenameShort"] = ns.shrinkPath(row["filename"]);
        html += mustache.render(ns.TMPL, row);
      });
      $("#report-body").html(html);
    }
  };

  ns.getProblems = function () {
    $.getJSON(ns.APIURL, function (data) {
      ns.data = data;
      ns.data.sort(ns.sortPredicate);
      $(ns).trigger("problems:updated");
    });
  };

  $(ns).on("problems:updated", ns.render);

  $(function () {
    $("#report-header").bind("keyup,blur,change", function () {
      ns.messageFilter = $("#message-filter").val();
      ns.pathFilter = $("#path-filter").val();
      $(ns).trigger("problems:updated");
    });
    ns.getProblems();

    $("#report-body").on("mouseenter", "td.filename", function () {
      $(this).text($(this).data("full"));
    }).on("mouseleave", "td.filename", function () {
      $(this).text($(this).data("short"));
    });

    setInterval(ns.getProblems, ns.REFRESH_DELAY);
  });

}(jQuery, Mustache));
