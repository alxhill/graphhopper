/**
 * Created by alexander on 22/02/2016.
 */
function initMap() {
    window.carIcon = L.icon({
        iconUrl: 'car-icon.png',
        iconSize: [20,15]
    });

    window.map = L.map('map').setView([51.505, -0.09], 13);
    L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
        attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
        maxZoom: 18,
        id: 'alxhill.p69lilm1',
        accessToken: 'pk.eyJ1IjoiYWx4aGlsbCIsImEiOiJjaWtyMnM5cTAwMDFzd2RrcWxjdW14dGlhIn0._vGArimDzlTVhET5T_GZzA'
    }).addTo(window.map);
}

function initButtons(carSet) {
    document.getElementById("startSimulation").addEventListener("click", function() {
        var count = document.getElementById("initVehicles").value;
        console.log("Starting simulation with", count, "vehicles");
        carSet.init(count);
    });
    document.getElementById("addVehicle").addEventListener("click", function () {
        carSet.ws.send("addVehicle");
    });
}

function initAll() {
    initMap();
    initButtons(carSet);
}


var carSet = {
    _cars: [],

    // start listening to the websocket and setup the callbacks
    init: function(count) {
        if (this.ws)
            this.ws.close();

        if (this._cars) {
            this._cars.forEach(function (c) {
                c.remove();
            });
            this._cars = [];
        }

        this.ws = new WebSocket("ws://localhost:8888");
        this.ws.onmessage = function (e) {
            this.processData(e.data);
        }.bind(this);
        this.ws.onopen = function (e) {
            this.ws.send("start|" + count);
        }.bind(this);
    },

    // deals with the set of data that comes from the websocket
    processData: function(data) {
        var msgs = data.split(",");
        for (var i = 0; i < msgs.length; i++) {
            var msg = msgs[i].split("|");
            var lat = parseFloat(msg[1]);
            var lon = parseFloat(msg[2]);
            var vel = parseInt(msg[3]);
            var index = parseInt(msg[0]);
            if (this._cars[index]) {
                this._cars[index].moveTo(lat, lon, vel);
            } else {
                this._cars[index] = new Car(index, vel, lat, lon);
            }
        }
    }
};

var Car = function(id, vel, lat, lon) {
    this.id = id;
    this.vel = vel;
    this.pos = [lat, lon];
    this.moveTo(lat, lon);
};

Car.prototype = {

    moveTo: function(lat, lon, vel) {
        var line = [this.pos, [lat, lon]];

        this.vel = vel;
        this.pos = [lat, lon];

        if (this.marker == null) {
            this.marker = L.animatedMarker(line, {
                icon: carIcon,
                distance: 1,
                interval: 1000,
                clickable: true
            }).on("click", function () {
                this.labelVisible = !this.labelVisible;
            }.bind(this));
            window.map.addLayer(this.marker);
        } else {
            this.marker.stop();
            this.marker.setLine(line);
        }
        this.marker.bindLabel(""+this.id + "|" + this.vel, {noHide: false});
        this.marker.start();
    },

    remove: function() {
        if (this.marker)
            window.map.removeLayer(this.marker);
    }
}