/*
 * Accept requests such as /hello?name=Ben
 */
function execute(parameters) {
	/*
	 * Tests that we can receive query parameters injected per request and
	 * access the global "api" reference to APIServiceLayer for invoking Java code
	 */
	return api.hello(parameters.name[0]); // uses "name" query parameter
}