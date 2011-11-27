$(function() {
    var debug = true;
    var intervalTimer;
    var pollInterval = 1500; // milliseconds
    var previousMatchedPortion = "";
    var running = false;
    var simulationId;


    function createSimulation() {
        $.ajax("/newSimulation", {
            contentType: "application/json",
            success: onNewSimulation
        });
    }

    function getSimulationStatus() {
        $("#debug").append("getSimulationStatus(): About to invoke status<br/>\n");
        $.ajax("/status/" + simulationId, {
            contentType: "application/json",
            success: onGetSimulationStatus,
            error: onError
        });
    }

    function onError(jqXHR, textStatus, errorThrown) {
        $("#debug").append("onError: " + textStatus + " "  + errorThrown +"<br/>\n")
    }

    // data will be similar to:
    // {"result":{"complete":false,"simulationId":"a2f57249-d739-49c5-b54b-596623013de7","length":0,
    //   "formattedElapsedTime":"00:00:00","formattedTimeStarted":"02:10:40 PM","maxTicks":100,"percentComplete":0,
    //   "tick":0,"monkeys":10,"matchedPortion":"","version":"0.2"}}
    function onGetSimulationStatus(data) {
        if (data.result) {
            simulationId = data.result.simulationId;
            $("#debug").append("Tick " + data.result.tick +": " + data.result.matchedPortion + "<br/>\n")
                       .prop({ scrollTop: $("#debug").prop("scrollHeight") });
            var percentMatched = (100.0 * data.result.matchedPortion.length) / data.result.documentLength;
            $("#debug").append("percentMatched: " + percentMatched + "<br/>");
            $("#documentLength") .html(data.result.documentLength);
            $("#version")        .html(data.result.version);
            $("#simulationId")   .html(data.result.simulationId);
            $("#started")        .html(data.result.formattedTimeStarted);
            $("#elapsed")        .html(data.result.formattedElapsedTime);
            $("#tick")           .html(Math.min(data.result.tick, data.result.maxTicks));
            $("#maxTicks")       .html(data.result.maxTicks);
            $("#match")          .html(data.result.matchedPortion.length);
            $("#percentComplete").html(data.result.percentComplete);
            $("#monkeys")        .html(data.result.monkeys);
            var newMatchedPortion = data.result.matchedPortion.substring(previousMatchedPortion.length);
            $("#matchedPortion") .html(previousMatchedPortion + '<span class="newMatchedPortion">' + newMatchedPortion + '</span>');
            $("#debug").append("data.result.matchedPortion=" + data.result.matchedPortion + "<br/>\n");
            $("#debug").append("previousMatchedPortion=" + previousMatchedPortion + "<br/>\n");
            $("#debug").append("newMatchedPortion=" + newMatchedPortion + "<br/>\n");
            $("#debug").append("complete=" + data.result.complete + "<br/>\n");
            $("#progressTick").progressbar("value", data.result.percentComplete);
            $("#progressMatched").progressbar("value", percentMatched);

            previousMatchedPortion = data.result.matchedPortion;
            if (data.result.tick>=data.result.maxTicks || data.result.complete==true) {
                if (intervalTimer)
                    window.clearInterval(intervalTimer);
                intervalTimer = undefined;
                $("#debug").append("Polling should stop<br/>\n");
                $("#stopSimulationButton").hide("slow");
                $("#newSimulationButton").show("slow");
                running = false;
            }
        }
    }

    function onNewSimulation(data) {
        if (data.simulationId) {
            simulationId = data.simulationId;
            //$("#debug").html("")
            $("#newSimulationButton").hide();
            $("#stopSimulationButton").click(stopSimulation);
            $("#stopSimulationButton").show();
            $("#results").show();
            runSimulation();
        }
    }

    function onRunSuccess(data) {
        if (debug==true)
            $("#debug").append("onRunSuccess(): About to check for data.result<br/>\n");
        if (data.result) {
            onGetSimulationStatus(data)
            intervalTimer = window.setInterval(getSimulationStatus, pollInterval);
        }
    }

    function onStopSuccess(data) {
        if (debug==true)
            $("#debug").append("onRunSuccess(): About to check for data.result<br/>\n");
        if (data.result) {
            onGetSimulationStatus(data)
        }
        if (intervalTimer)
            window.clearInterval(intervalTimer);
        intervalTimer = undefined;
    }

    function runSimulation() {
        running = true;
        previousMatchedPortion = "";
        if (debug==true)
            $("#debug").show()
        $("#debug").append("runSimulation(): About to invoke run<br/>\n");
        $.ajax("/run/" + simulationId, {
            contentType: "application/json",
            success: onRunSuccess,
            error: onError
        });
    }

    function stopSimulation() {
        $("#debug").append("Asking Hanuman to stop simulation " + simulationId + "<br/>\n");
        running = false;
        $("#debug").html();
        $("#stopSimulationButton").hide();
        $("#newSimulationButton").show();
        $.ajax("/stop/" + simulationId, {
            contentType: "application/json",
            success: onStopSuccess,
            error: onError
        });
    }

    function toggleDebug(){
        debug = !debug;
        if (debug==true && running==true)
           $("#debug").show("slow");
        else
           $("#debug").hide("slow");
    }

    $("body")
        .append('<h1 style="font-family: arial">Hanuman <span id="version"></span></h1>\n')
        .append('<p><a href="https://github.com/mslinn/hanuman" target="src">Source on GitHub</a><br/>' +
                '<a href="http://www.slideshare.net/mslinn/hanuman-10278606" target="vid">' +
                'Presentation on SlideShare</a></p>\n')
        .append('<button id="newSimulationButton">Start a new simulation</button>\n')
        .append('<div id="results" class="results"></div>\n')
        .append('<div id="debug" class="debug"></div>\n')
        .append('<a href="http://micronauticsresearch.com" target="mr">' +
                '<img src="http://micronauticsresearch.com/images/logo-300x91.png" ' +
                'style="position: absolute; bottom: 5; left: 5" /></a>\n')
        .append('<div class="debugCheckbox">Debug <input type="checkbox" id="debugCheckbox" />\n</div>\n')
        .append('<a href="http://heroku.com" target="h">' +
                '<img src="https://nav.heroku.com/images/logos/logo.png" ' +
                'style="position: absolute; bottom: 5; right: 5" /></a>\n');
    $("#debug").hide()
    $("#results").append('<div></div><span class=label>Simulation ID</span> <span id="simulationId" ' +
                         'style="font-family: courier;mono">' + simulationId + '</span> &nbsp;&nbsp;')
                 .append('<button id="stopSimulationButton">Stop simulation</button></div>\n')
                 .append('<br/><br/>\n')
                 .append('<div></div><span class=label>Started at</span> <span id="started"></span>; ' +
                         '<span class=label>elapsed time</span> <span id="elapsed">00:00:00</span><span class=label>; ' +
                         'simulating</span> <span id="monkeys">0</span> ' +
                         '<span class=label>monkeys typing semi-randomly.</span></div>\n')
                 .append('<div style="margin-top: 12pt"><span class=label>Tick</span> <span id="tick">0</span> ' +
                         '<span class=label>of</span> <span id="maxTicks"></span>; ' +
                         '<span id="percentComplete"></span><span class=label>% complete:</span></div>\n')
                 .append('<div id="progressTick" width="200" class="progressBar"></div>')
                 .append('<div style="margin-top: 12pt"><span id="match">0</span> ' +
                         '<span class=label>characters of</span> <span id="documentLength">0</span> ' +
                         '<span class=label>matched so far:</span></div>\n')
                 .append('<div id="progressMatched" width="200" class="progressBar"></div>')
                 .append('<div id="matchedPortion" class="matchedPortion"></div>\n').hide();
    $("#progressTick")   .progressbar();
    $("#progressMatched").progressbar();
    $("#newSimulationButton").click(createSimulation);
    $("#debugCheckbox").click(toggleDebug);
})