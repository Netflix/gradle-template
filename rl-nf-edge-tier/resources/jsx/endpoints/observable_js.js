/*
 * Prove we can return Java types.
 * TODO Explore interaction between RxJava and RxJS 
 */
function execute(parameters) {
	return Rx.Observable.fromArray(["one", "two", "three"])
}