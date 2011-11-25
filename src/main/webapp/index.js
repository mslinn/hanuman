$(function() {
    var simulationId;
    var debug = false;

    function createSimulation() {
        $.ajax("/newSimulation", {
            contentType: "application/json",
            success: handleCreateSimulation
        });
    }

    function handleCreateSimulation(data) {
        if (data.id) {
            $("#newSimulationButton").remove();
            $("body").append('<div id="results"></div>');
            simulationId = data.id;
            $("#results").append('<h1 style="font-family: arial">Hanuman v<span id="version"></span></h1>\n');
            $("#results").append('<b>Simulation:</b> <tt>' + simulationId + '</tt><br/>\n');
            $("#results").append('<b>Started:</b> <span id="started"></span>; elapsed time: <span id="elapsed">00:00:00</span><br/>\n');
            $("#results").append('<b>Tick</b> <span id="tick">0</span> of <span id="maxTicks"></span>; <span id="percentComplete"></span> % complete <br/>\n');
            $("#results").append('<span id="match">0</span> characters matched so far<br/>\n');
            $("#results").append('<div id="portion" style="font-style: italic; margin-top: 6pt"></div>\n');
            runSimulation();
        }
    }

    function runSimulation() {
        if (debug==true)
            $("#results").append("runSimulation(): About to invoke run<br/>\n");
        $.ajax("/run/" + simulationId, {
            contentType: "application/json",
            success: onRunSuccess,
            error: onError
        });
    }

    function onError(jqXHR, textStatus, errorThrown) {
        $("#results").append("onError: " + textStatus + " "  + errorThrown +"<br/>\n")
    }

    function onRunSuccess(data) {
        if (debug==true)
            $("#results").append("onRunSuccess(): About to check for data.result<br/>\n");
        if (data.result) {
            //$("#results").append("<li>" + data.result + "</li>");
            onGetSimulationStatus(data)
            window.setInterval(getSimulationStatus, 1500);
        }
    }

    function getSimulationStatus() {
        if (debug==true)
            $("#results").append("<br/>\ngetSimulationStatus(): About to invoke status<br/>\n");
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
            $("#complete")       .html(data.result.complete);
            $("#started")        .html(data.result.formattedTimeStarted);
            $("#elapsed")        .html(data.result.formattedElapsedTime);
            $("#tick")           .html(data.result.tick);
            $("#maxTicks")       .html(data.result.maxTicks);
            $("#percentComplete").html(data.result.percentComplete);
            $("#monkeys")        .html(data.result.monkeys);
            $("#matchedPortion") .html(data.result.matchedPortion);
        }
    }

    $("body").append('<button id="newSimulationButton">Start a new simulation</button>');
    $("#newSimulationButton").click(createSimulation);
});