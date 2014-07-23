;(function (root, $, mustache) {

  if (typeof root.eclimHttp === "undefined") {
    root.eclimHttp = {};
  }
  
  var ns = {
    APIURL: "/data/project_list",
    TEMPLATE: "/js/templates/project-selector-option.html"
  };

  ns.ProjectSelector = function (elementId) {
    var self = this;
    this.elementId = elementId;
    this.data = [];
    this.template = null;
    $("#" + elementId).on("change", function () {
      self.selectedProject = self.data[parseInt($(this).val(), 10)];
      $(self).trigger("project-selector:change");
    });
  };

  ns.ProjectSelector.prototype.getProjects = function () {
    var self = this;
    $.getJSON(ns.APIURL, function (data) {
      self.data = data;
      self.data.splice(0, 0, {"name": "Select One"});
      $(self).trigger("project-selector:updated");
      self.render();
    });
  };

  ns.ProjectSelector.prototype.render = function () {
    var html, self = this;
    if (this.template === null) {
      $.get(ns.TEMPLATE, function (data) {
        self.template = data;
        self.render();
      });
    } else {
      html = "";
      $.each(this.data, function (i, row) {
        row["index"] = i;
        html += mustache.render(self.template, row);
      });
      $("#" + this.elementId).html(html);
    }
  };

  root.eclimHttp.projectSelector = ns;

}(this, jQuery, Mustache));
