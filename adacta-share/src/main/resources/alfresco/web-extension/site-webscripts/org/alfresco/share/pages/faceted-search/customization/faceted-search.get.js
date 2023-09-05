// Add JavaScript to modify the JSON model for the page
var widget = widgetUtils.findObject(model.jsonModel.widgets, "id", "FCTSRCH_SEARCH_RESULTS_LIST");
widget.config.spellcheck=false;
