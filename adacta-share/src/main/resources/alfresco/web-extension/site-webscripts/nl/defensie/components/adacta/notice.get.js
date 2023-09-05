function main() {

	var text = remote.call("/nl/defensie/adacta/notice");	
    model.text = stringUtils.stripUnsafeHTML(text);
}

main();