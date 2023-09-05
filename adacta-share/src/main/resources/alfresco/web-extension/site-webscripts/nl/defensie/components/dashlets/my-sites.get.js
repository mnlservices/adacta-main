// OVERRIDES FOR ALFRESCO 5.1
// - Hide the create site button if not in specific groups
function main()
{
   // Prepare the model for the template
   model.imapServerEnabled = imapServerStatus.enabled;
   
   // Widget instantiation metadata...
   var dashboardconfig = config.scoped['Dashboard']['dashboard'];
   var listSize = dashboardconfig.getChildValue('summary-list-size');
   if (listSize == null)
   {
      listSize = 100;
   }
   
   var mySites = {
      id : "MySites", 
      name : "Alfresco.dashlet.MySites",
      options : {
         imapEnabled : imapServerStatus.enabled,
         listSize : parseInt(listSize),
         regionId : args['region-id']
      }
   };
   
   var dashletResizer = {
      id : "DashletResizer", 
      name : "Alfresco.widget.DashletResizer",
      initArgs : ["\"" + args.htmlid + "\"", "\"" + instance.object.id + "\""],
      useMessages : false
   };
   
   var dashletTitleBarActions = {
      id : "DashletTitleBarActions", 
      name : "Alfresco.widget.DashletTitleBarActions",
      useMessages : false,
      options : {
         actions: [
            {
               cssClass: "help",
               bubbleOnClick:
               {
                  message: msg.get("dashlet.help")
               },
               tooltip: msg.get("dashlet.help.tooltip")
            }
         ]
      }
   };
   model.widgets = [mySites, dashletResizer, dashletTitleBarActions];
   model.showCreateSite = canCreateSite();
}
function canCreateSite() {
    var canCreate = false;
    var json = remote.call("/api/people/" + user.name + "?groups=true");
    if (json.status == 200) {
        var result = JSON.parse(json);
        for each(var group in result.groups) {
            if (group.itemName === "GROUP_SITE_CREATORS"  || group.itemName === "GROUP_ALFRESCO_ADMINISTRATORS") {
                canCreate = true;
                break;
            }
        }
    }
    return canCreate;
}
main();