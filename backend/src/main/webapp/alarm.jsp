<%@ page import="com.meiste.tempalarm.backend.Constants"%>
<%@ page import="com.meiste.tempalarm.backend.SettingUtils"%>
<%@ page import="com.meiste.tempalarm.backend.TemperatureCommon"%>

<%
    String lowThres = request.getParameter("lowThres");
    if (lowThres != null) {
        /* Normalize the submitted value */
        lowThres = String.format("%.1f", Float.parseFloat(lowThres));

        SettingUtils.setValue(Constants.SETTING_THRES_LOW, lowThres);
        response.sendRedirect("index.jsp");
    }
%>

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
                <h3>Set new alarm threshold</h3>
                <form id="edit_alarm" method="post">
                    <input type="number" name="lowThres" min="32.0" max="90.0" step="0.5"
                        value="<%= TemperatureCommon.getLowTempThreshold() %>">

                    <input type="submit">
                </form>
            </div>
        </div>
    </div>
</div>

</body>
</html>
