<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE HTML>
<html>
<head>
<script type="text/javascript" src="js/jquery-1.7.2.js"></script>
<script type="text/javascript">
$(document).ready(function(){
	//var json = null;
	$.get("autoreply?action=get",
		function(data){
			$("#main_container").text(data);
		}
	);
});
</script>
</head>
<body>
	<jsp:include page="head.jsp" flush="true" />
	<div id="main">
		<div id="main_container">
		
		</div>
	</div>
	<jsp:include page="foot.jsp" flush="true" />
</body>
</html>