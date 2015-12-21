<%@ page import="com.meiste.tempalarm.backend.TemperatureCommon"%>
<%@ page import="com.meiste.tempalarm.backend.TemperatureRecord"%>

<%@ page import="static com.meiste.tempalarm.backend.OfyService.ofy"%>

<!DOCTYPE html>
<html>
<head>
    <title>Shop Temperature Alarm</title>
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
                    <h1><%= temp.getDegF() %>&deg;F</h1>
                    <h2><%= temp.getHumidity() %>% humidity</h2>
                    <p><%= temp.getDateTimeString() %></p>
<%
                } else {
%>
                    <p>No data available</p>
<%
                }
%>
                <p>Alarm set to <a href="alarm.jsp"><%= TemperatureCommon.getLowTempThreshold() %>&deg;F</a></p>
            </div>
        </div>
    </div>
</div>

</body>
</html>
