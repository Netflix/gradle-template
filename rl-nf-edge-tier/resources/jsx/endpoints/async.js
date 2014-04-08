/*
 * Prove we can handle the rx.Observable from Java, perform transformations and return it
 */
function execute(parameters) {
	return api.getData().map(function(s) {
		return "transformed-data-" + s;
	})
}