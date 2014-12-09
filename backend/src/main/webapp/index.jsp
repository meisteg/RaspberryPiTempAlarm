<%@ page import="com.meiste.tempalarm.backend.TemperatureRecord"%>

<%@ page import="static com.meiste.tempalarm.backend.OfyService.ofy"%>

<!DOCTYPE html>
<html>
<head>
    <title>Raspberry Pi Temperature Alarm</title>
    <link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css">
</head>
<body role="document" style="padding-top: 70px;">

<div class="container theme-showcase" role="main">
    <div class="jumbotron">
        <div class="row">
            <div class="col-lg-12" style="text-align: center;">
<%
                final TemperatureRecord temp =
                    ofy().load().type(TemperatureRecord.class).order("-timestamp").first().now();
                if (temp != null) {
%>
                <h1><%= temp.getDegF() %> degrees</h1>
<%
                if (temp.areLightsOn()) {
%>
                <p>Lights are ON</p>
<%
                } else {
%>
                <p>Lights are OFF</p>
<%
                }
%>
                <p><%= temp.getRelativeTimeSpanString() %></p>
<%
                } else {
%>
                <h1>N/A degrees</h1>
                <p>No data available</p>
<%
                }
%>
            </div>
        </div>
    </div>
</div>

</body>
</html>
