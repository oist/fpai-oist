$(window).load(function() {
	w = new widget("update", 1000, function(data) {
		$("#loading").detach();
		$("p").show();
		$(".error").hide();
		$("#houseId").text(data.houseId);
		$("#soc").text(data.soc);
		$("#command").text(data.command);
	});
	
});