function main() {
    // Check for site existence
    var conn = remote.connect("alfresco");
    findSite(conn);
}

function findSite(conn) {
    var res = conn.get("/api/sites/adacta");
    if (res.status == 404) {
        // site does not exist yet
        model.foundsite = false;
    } else if (res.status == 200) {
        // site already exists
        model.foundsite = true;
    }
}

function widgets() {
    // Widget instantiation...
    var widget = {
        id: "Setup",
        name: "ADACTA.Setup",
        initArgs: ["\"" + args.htmlid + "\"", "\"" + instance.object.id + "\""],
        useMessages: true
    };
    model.widgets = [widget];
}

function globalProps(){
	var json = remote.call("/nl/defensie/adacta/globalproperties");
	
	if (json.status == 200) {
		var result = JSON.parse(json);
		model.serverMode = result.serverMode;
	}
}

widgets();
globalProps();
main();