function main(){adacta.info("Delete document library...");var b=adacta.getSiteDocumentLibrary();deleteFolders(b);adacta.info("Delete data lists...");var a=adacta.getDataListContainer();deleteFolders(a);adacta.info("Delete adacta scripts...");var c=adacta.getDictionaryScripts();deleteScripts(c);adacta.info("Done!")}function deleteScripts(c){var b=c.children;for(var a in b){var d=b[a];if(d.name.indexOf("adacta")>-1){d.remove()}}}function deleteFolders(c){var b=c.children;for(var a in b){var d=b[a];d.remove()}}main();