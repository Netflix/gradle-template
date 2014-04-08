/*
 * Demonstrate use of 3rd party library
 * TODO figure out how to dynamically load a library. Right now it is hardcoded on the Java side to include it.
 */
var view = {
	title : "Joe",
	calc : function() {
		return 2 + 4;
	}
};

function execute(parameters) {
	return Mustache.render("{{title}} spends {{calc}}", view); 
}
