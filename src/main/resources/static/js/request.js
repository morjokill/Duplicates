setQueueInfo();

(function updateQueue() {
    setTimeout(function () {
        setQueueInfo();
        updateQueue();
    }, 500);
})();

setLibs();

(function updateLibs() {
    setTimeout(function () {
        setLibs();
        updateLibs();
    }, 10000);
})();

document.getElementById("indexButton").addEventListener("click", function () {
    putInQueue();
});

document.getElementById("checkDocument").addEventListener("click", function () {
    checkDocument();
});

function checkDocument() {
    console.log("CHECKING");
    var text = document.getElementById("documentText").value;
    text = text.replace(/(?:\r\n|\r|\n)/g, '<br>');
    var library = $('#pickedLib').html();
    library = library.substring(0, library.length - 27);
    console.log("PICKED: " + library);
    document.getElementById("documentText").value = '';
    var response = sendPostRequest('http://localhost:8080/check', '{"text": "' + text + '", "library": "' + library + '"}');
    var doubles = parseJson(response);
    if (doubles.length > 0) {
        alert(JSON.stringify(doubles));
    } else {
        alert("NO DOUBLES");
    }
}

function setLibs() {
    console.log("SET LIBS");
    $('#indexedLibs').empty();
    var data = getLibs();
    data = parseJson(data);
    $.each(data, function (i, option) {
        $('#indexedLibs').append('<li><a href="#">' + option.url + '</a></li>');
    });
    $('#indexedLibs a').on('click', function () {
        $('.dropdown-toggle').html($(this).html() + '<span class="caret"></span>');
    });
}

function getLibs() {
    return sendGetRequest('http://localhost:8080/libs');
}

function putInQueue() {
    console.log("PUT IN QUEUE");
    var library = document.getElementById("libraryUrl").value;
    var clarification = document.getElementById("clarification").value;
    document.getElementById("libraryUrl").value = '';
    document.getElementById("clarification").value = '';
    sendPostRequest('http://localhost:8080/queue', '{"library": "' + library + '", "clarifications": ["' + clarification + '"]}');
}

function setQueueInfo() {
    console.log("SET QUEUE");
    var queue = getQueue();
    document.getElementById("queueSize").innerHTML = queue.queueSize;
    document.getElementById("currentLib").innerHTML = queue.library;
    document.getElementById("parsedDocs").innerHTML = queue.parsedLinks;
    document.getElementById("allDocs").innerHTML = queue.allLinks;

    if (queue.queueSize !== 0) {
        document.getElementById("queueHead").style.display = "block";
    } else {
        document.getElementById("queueHead").style.display = "none";
    }
}

function getQueue() {
    var json = sendGetRequest('http://localhost:8080/queue');
    return parseJson(json);
}

function sendPostRequest(url, data) {
    var req = new XMLHttpRequest();
    req.open("POST", url, false);
    req.setRequestHeader('Content-Type', 'application/json; charset=UTF-8');
    req.send(data);
    if (req.status !== 200) {
        console.log(req.status + ': ' + req.statusText);
    } else {
        console.log(req.responseText);
    }
    return req.responseText;
}

function sendGetRequest(url) {
    var req = new XMLHttpRequest();
    req.open("GET", url, false);
    req.send(null);
    if (req.status !== 200) {
        console.log(req.status + ': ' + req.statusText);
    } else {
        console.log(req.responseText);
    }
    return req.responseText;
}

function parseJson(json) {
    return JSON.parse(json);
}