/**
 * Script that will set authorization for all existing users in Alfresco. It will set the DP codes that are stored in the external (autorization) database.
 */

function main() {

	var action = actions.create("adactaMigrationUserFolderSynchronizer");
	action.parameters.step = 10000;
	action.execute(search.luceneSearch("PATH:\"/app:company_home\"")[0]);
}

main();