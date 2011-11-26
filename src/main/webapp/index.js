$(function() {
    var simulationId;
    var debug = false;

    function createSimulation() {
        $.ajax("/newSimulation", {
            contentType: "application/json",
            success: onCreateSimulation
        });
    }

    function onCreateSimulation(data) {
        if (data.id) {
            simulationId = data.id;
            $("#newSimulationButton").hide();
            //$("#results").html()
            $("#stopSimulationButton").click(stopSimulation);
            $("#stopSimulationButton").show();
            $("#results").show();
            runSimulation();
        }
    }

    function runSimulation() {
        if (debug==true)
            $("#debug").append("runSimulation(): About to invoke run<br/>\n");
        $.ajax("/run/" + simulationId, {
            contentType: "application/json",
            success: onRunSuccess,
            error: onError
        });
    }

    function onError(jqXHR, textStatus, errorThrown) {
        if (debug==true)
            $("#debug").append("onError: " + textStatus + " "  + errorThrown +"<br/>\n")
    }

    function onRunSuccess(data) {
        if (debug==true)
            $("#debug").append("onRunSuccess(): About to check for data.result<br/>\n");
        if (data.result) {
            //$("#results").append("<li>" + data.result + "</li>");
            onGetSimulationStatus(data)
            window.setInterval(getSimulationStatus, 1500);
        }
    }

    function getSimulationStatus() {
        if (debug==true)
            $("#debug").append("<br/>\ngetSimulationStatus(): About to invoke status<br/>\n");
        $.ajax("/status/" + simulationId, {
            contentType: "application/json",
            success: onGetSimulationStatus,
            error: onError
        });
    }

    // data will be similar to:
    // {"result":{"complete":false,"id":"a2f57249-d739-49c5-b54b-596623013de7","length":0,"formattedElapsedTime":"00:00:00","formattedTimeStarted":"02:10:40 PM","maxTicks":100,"percentComplete":0,"tick":0,"monkeys":10,"matchedPortion":"","version":"0.2"}}
    function onGetSimulationStatus(data) {
        if (data.result) {
            $("#version")        .html(data.result.version);
            $("#started")        .html(data.result.formattedTimeStarted);
            $("#elapsed")        .html(data.result.formattedElapsedTime);
            $("#tick")           .html(data.result.tick);
            $("#maxTicks")       .html(data.result.maxTicks);
            $("#percentComplete").html(data.result.percentComplete);
            $("#monkeys")        .html(data.result.monkeys);
            $("#matchedPortion") .html(data.result.matchedPortion);
            if (data.result.complete) {
                $("#stopSimulationButton").hide("slow");
                $("#newSimulationButton").show("slow");
                // do other things to show user simulation is done
                // stop querying server
            }
        }
    }

    function stopSimulation() {
        $("#debug").html();
        $("#results").hide();
        $("#stopSimulationButton").hide();
        $("#newSimulationButton").show();
        $.ajax("/stop", {
            contentType: "application/json"
        });
    }

    $("body")
        .append('<h1 style="font-family: arial">Hanuman <span id="version"></span></h1>\n')
        .append('<button id="newSimulationButton">Start a new simulation</button>\n')
        .append('<div id="results"></div>\n')
        .append('<div id="debug"></div>\n')
        .append('<a href="http://micronauticsresearch.com" target="mr"><img src="http://micronauticsresearch.com/images/logo-300x91.png" style="position: absolute; bottom: 5; left: 5" /></a>\n')
        .append('<a href="http://heroku.com" target="h"><img src="https://nav.heroku.com/images/logos/logo.png" style="position: absolute; bottom: 5; right: 5" /></a>\n');
    $("#results").append('<span class=label>Simulation ID</span> <tt>' + simulationId + '</tt> &nbsp;&nbsp;')
                 .append('<button id="stopSimulationButton">Stop simulation</button>\n')
                 .append('<br/><br/>\n')
                 .append('<span class=label>Started at</span> <span id="started"></span>; <span class=label>elapsed time</span> <span id="elapsed">00:00:00</span><br/><br/>\n')
                 .append('<span class=label>Tick</span> <span id="tick">0</span> of <span id="maxTicks"></span>; <span id="percentComplete"></span> % complete <br/><br/>\n')
                 .append('<span id="match">0</span> characters matched so far:<br/>\n')
                 .append('<div id="matchedPortion" class="matchedPortion"></div>\n').hide();
    $("#newSimulationButton").click(createSimulation);
})