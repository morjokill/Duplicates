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

$('#indexButton').on('click', function () {
    var $this = $(this);
    $this.button('loading');
    putInQueue();
});

$('#checkDocument').on('click', function () {
    var $this = $(this);
    $this.button('loading');
    checkDocument();
});

$('#removeButton').on('click', function () {
    var $this = $(this);
    $this.button('loading');
    removeFromQueue();
});

$('#checkFile').on('click', function () {
    var $this = $(this);
    $this.button('loading');
    sendFileToCheck();
});

var data;

function sendFileToCheck() {
    console.log("SENDING FILE");
    $('#checkAlert').empty();
    var library = $('#pickedLib').html();
    library = library.replace('<span class="caret"></span>', '');
    library = library.replace(/(?:\r\n|\r|\n)/g, '');
    library = library.replace(/[|&;$%@"<>()+,]/g, '');
    console.log("PICKED: " + library);

    let file = document.getElementById("documentFile").files[0];
    console.log("PICKED FILE: " + file);
    let formData = new FormData();
    formData.append("file", file);
    formData.append("library", library);

    sendFile('http://localhost:8080/checkFile', formData, 20000).then(function (value) {
        document.getElementById("documentFile").value = '';
        console.log("DOUBLES: " + value);
        var doubles = parseJson(value);
        if (doubles.length > 0) {
            $.each(doubles, function (i, option) {
                $('#checkAlert').append('<div class="alert alert-danger" role="alert">Double: ' + option.url + '</div>');
            });
            $('#checkFile').button('reset');
        } else {
            $('#checkAlert').append('<div class="alert alert-success" role="alert">No doubles!</div>');
            $('#checkFile').button('reset');
        }
    }).catch(function (reason) {
        alert("Could not check document. Server is not available");
        $('#checkFile').button('reset');
    });
}

function checkDocument() {
    console.log("CHECKING");
    $('#checkAlert').empty();
    var text = document.getElementById("documentText").value;
    text = text.replace(/(?:\r\n|\r|\n)/g, '<br>');
    text = text.replace(/[\\|&;$%@"<>()+,]/g, ' ');
    var library = $('#pickedLib').html();
    library = library.replace('<span class="caret"></span>', '');
    library = library.replace(/(?:\r\n|\r|\n)/g, '');
    library = library.replace(/[|&;$%@"<>()+,\\]/g, '');
    console.log("PICKED: " + library);
    sendPostRequest('http://localhost:8080/check', '{"text": "' + text + '", "library": "' + library + '"}', 20000).then(function (value) {
        document.getElementById("documentText").value = '';
        console.log("DOUBLES: " + value);
        var doubles = parseJson(value);
        if (doubles.length > 0) {
            $.each(doubles, function (i, option) {
                $('#checkAlert').append('<div class="alert alert-danger" role="alert">Double: ' + option.url + '</div>');
            });
            $('#checkDocument').button('reset');
        } else {
            $('#checkAlert').append('<div class="alert alert-success" role="alert">No doubles!</div>');
            $('#checkDocument').button('reset');
        }
    }).catch(function (reason) {
        alert("Could not check document. Server is not available");
        $('#checkDocument').button('reset');
    });
}

function setLibs() {
    console.log("SET LIBS");
    $('#indexedLibs').empty();
    getLibs().then(function (value) {
        data = parseJson(value);
        $.each(data, function (i, option) {
            $('#indexedLibs').append('<li><a href="#" id="indexedLib' + i + '">' + option.url + '</a></li>');
        });
        var html = $('.dropdown-toggle').html();
        html = html.replace(/(?:\r\n|\r|\n)/g, '');
        console.log("DROPDOWN: " + html);
        if (data.length > 0 && html === 'Library to check into<span class="caret"></span>') {
            $('#indexedLib0').trigger('click');
        } else {
            if (data.length === 0) {
                document.getElementById("pickedLibrary").style.display = "none";
            }
        }
        $('#indexedLibs a').on('click', function () {
            var id = this.id;
            id = id.replace('indexedLib', '');
            console.log("PICKED ID: " + id);
            console.log("DATA: " + data);
            $('.dropdown-toggle').html($(this).html() + '<span class="caret"></span>');
            console.log("DATA[ID].url = " + data[id].url);
            document.getElementById("pickedUrl").innerHTML = data[id].url;
            document.getElementById("pickedParsedDate").innerHTML = data[id].lastTimeParsed;
            document.getElementById("pickedWordsCount").innerHTML = data[id].wordsCount;
            document.getElementById("pickedLibrary").style.display = "table";
        });
    }).catch(function (reason) {
        console.log("Server is not available: " + reason)
    });
}

function getLibs() {
    return sendGetRequest('http://localhost:8080/libs');
}

function removeFromQueue() {
    console.log("REMOVE FROM QUEUE");
    var uuid = document.getElementById("libraryUuid").value;
    sendPostRequest('http://localhost:8080/remove', uuid, 5000).then(function (value) {
        $('#removeButton').button('reset');
    }).catch(function (reason) {
        $('#removeButton').button('reset');
        console.log("Server is not available: " + reason)
    });
}

function putInQueue() {
    console.log("PUT IN QUEUE");
    var library = document.getElementById("libraryUrl").value;
    var clarification = document.getElementById("clarification").value;
    document.getElementById("libraryUrl").value = '';
    document.getElementById("clarification").value = '';
    sendPostRequest('http://localhost:8080/queue', '{"library": "' + library + '", "clarifications": ["' + clarification + '"]}', 5000).then(function (value) {
        console.log("PUT IN QUEUE VALUE: " + value);
        document.getElementById("libraryUuid").value = value;
        $('#indexButton').button('reset');
    }).catch(function (reason) {
        $('#indexButton').button('reset');
        console.log("Server is not available: " + reason)
    });
}

function setQueueInfo() {
    console.log("SET QUEUE");
    getQueue().then(function (value) {
        var queue = parseJson(value);
        console.log("AFTER GET Q: " + queue.queueSize);
        document.getElementById("queueSize").innerHTML = queue.queueSize;
        document.getElementById("currentLib").innerHTML = queue.library;
        document.getElementById("parsedDocs").innerHTML = queue.parsedLinks;
        document.getElementById("allDocs").innerHTML = queue.allLinks;
        document.getElementById("status").innerHTML = queue.status;

        if (queue.queueSize !== 0) {
            document.getElementById("queueHead").style.display = "table";
        } else {
            document.getElementById("queueHead").style.display = "none";
        }
    }).catch(function (reason) {
        document.getElementById("queueSize").innerHTML = '0';
        document.getElementById("queueHead").style.display = "none";
        console.log("Server is not available or json is not valid: " + reason)
    });
}

function getQueue() {
    return sendGetRequest('http://localhost:8080/queue');
}

function sendGetRequest(url) {
    return sendRequest(url, null, 500, "GET", null);
}

function sendFile(url, data, timeout) {
    return sendRequest(url, data, timeout, "POST", null);
}

function sendPostRequest(url, data, timeout) {
    return sendRequest(url, data, timeout, "POST", 'application/json; charset=UTF-8');
}

function sendRequest(url, data, timeout, method, contentType) {
    return new Promise(function (resolve, reject) {
        var req = new XMLHttpRequest();
        req.ontimeout = reject;
        req.onerror = reject;
        req.onload = function () {
            console.log(this.responseText);
            resolve(this.responseText);
        };
        req.open(method, url, true);
        if (contentType !== null) {
            req.setRequestHeader('Content-Type', contentType);
        }
        req.timeout = timeout;
        req.send(data);
    });
}

function parseJson(json) {
    return JSON.parse(json);
}