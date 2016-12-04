<%@ page import="com.meiste.tempalarm.backend.Constants"%>
<%@ page import="com.meiste.tempalarm.backend.SettingUtils"%>
<%@ page import="com.meiste.tempalarm.backend.TemperatureCommon"%>

<%
    String lowThres = request.getParameter("lowThres");
    String highThres = request.getParameter("highThres");
    if ((lowThres != null) && (highThres != null)) {
        /* Normalize the submitted values */
        lowThres = String.format("%.1f", Float.parseFloat(lowThres));
        highThres = String.format("%.1f", Float.parseFloat(highThres));

        if (Float.parseFloat(lowThres) < Float.parseFloat(highThres)) {
            SettingUtils.setValue(Constants.SETTING_THRES_LOW, lowThres);
            SettingUtils.setValue(Constants.SETTING_THRES_HIGH, highThres);
        }

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
                <h3>Set new alarm thresholds</h3>
                <form id="edit_alarm" method="post">
                    <label for="lowThres">Low</label>
                    <input type="number" id="lowThres" name="lowThres" min="32.0" max="90.0" step="0.5"
                        value="<%= TemperatureCommon.getLowTempThreshold() %>">
                    <label for="highThres">High</label>
                    <input type="number" id="highThres" name="highThres" min="50.0" max="100.0" step="0.5"
                        value="<%= TemperatureCommon.getHighTempThreshold() %>">
                    <br /><br />
                    <input type="submit">
                </form>
            </div>
        </div>
    </div>
</div>

</body>
</html>
