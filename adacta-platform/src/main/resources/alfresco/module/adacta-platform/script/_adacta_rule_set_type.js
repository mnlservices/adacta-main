/**
 * Inbound rule script set on index and import root folders (Adacta site). 
 */
if (document && document.typeShort != "ada:document") {
    document.specializeType("ada:document");
    
    // Set created date on custom adacta created date.    
    document.properties["ada:docDateCreated"] = document.properties["cm:created"];
    document.save();
}