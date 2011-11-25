$(function() {
    var simulationId;


    function createSimulation() {
        $.ajax("/newSimulation", {
            contentType: "application/json",
            success: handleCreateSimulation
        });
    }

    function handleCreateSimulation(data) {
        if (data.id) {
            $("#newSimulationButton").remove();
            $("body").append('<ul id="results"></ul>');
            simulationId = data.id;
            $("#results").prepend("<li>Created Simulation: " + simulationId + "</li>");
            runSimulation();
        }
    }

    function runSimulation() {
        $("#results").prepend("About to invoke run");
        $.ajax("/run/" + simulationId, {
            contentType: "application/json",
            success: onRunSuccess,
            error: onRunError
        });
    }

    function onRunError(jqXHR, textStatus, errorThrown) {
        $("#results").prepend("onRunError: " + textStatus + " "  + errorThrown +"<br/>\n")
    }

    function onRunSuccess(data) {
        $("#results").prepend("About to check for data.result");
        if (data.result) {
            $("#results").prepend("<li>" + data.result + "</li>");
            window.setInterval(getSimulationStatus, 1500);
        }
    }

    function getSimulationStatus() {
        $.ajax("/status/" + simulationId, {
            contentType: "application/json",
            success: handleGetSimulationStatus
        });
    }

    function handleGetSimulationStatus(data) {
        if (data.result) {
            $.each(data.result, function(index, item) {
                var output = "id = " + item.monkeyRef + " length = " + item.length + " startPos = " + item.startPos + " endPos = " + item.endPos;
                if ($("#" + item.monkeyRef).length) {
                    $("#" + item.monkeyRef).html(output);
                } else {
                    $("#results").prepend("<li id='" + item.monkeyRef +"'>" + output + "</li>");
                }
            });
        }
    }

    $("body").append('<button id="newSimulationButton">Start a new simulation</button>');
    $("#newSimulationButton").click(createSimulation);
});