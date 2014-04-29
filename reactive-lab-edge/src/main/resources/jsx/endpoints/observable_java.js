/*
 * Prove we can return Java types.
 * TODO Explore interaction between RxJava and RxJS 
 */
function execute(parameters) {
	return Java.type("rx.Observable").from("one", "two", "three")
}