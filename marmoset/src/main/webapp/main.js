/**
 * Created by alexander on 22/02/2016.
 */

function initMap() {

    window.canvas = document.getElementById("canvas");
    window.ctx = canvas.getContext("2d");
    window.icon = document.getElementById("icon");

    window.carIcon = L.icon({
        iconUrl: 'car-icon-opt.png',
        iconSize: [20,8]
    });

    window.map = L.map('map', {zoomAnimation:false}).setView([51.505, -0.09], 13);
    L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
        attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
        maxZoom: 18,
        id: 'alxhill.p69lilm1',
        accessToken: 'pk.eyJ1IjoiYWx4aGlsbCIsImEiOiJjaWtyMnM5cTAwMDFzd2RrcWxjdW14dGlhIn0._vGArimDzlTVhET5T_GZzA',
    }).addTo(window.map);

    //resizeCanvas(window.map.getSize());
    //window.map.on('moveend', function() {
    //    //resizeCanvas(window.map.getSize());
    //});
    //
    //window.addEventListener('resize', function(){
    //    //console.log("resize");
    //})

}

function resizeCanvas(size) {
    if (size.x != window.canvas.width)
        window.canvas.width = size.x;
    if (size.y != window.canvas.height)
        window.canvas.height = size.y;
}

function initButtons(carSet) {
    document.getElementById("startSimulation").addEventListener("click", function() {
        var count = document.getElementById("initVehicles").value;
        console.log("Starting simulation with", count, "vehicles");
        carSet.init(count);
    });

    document.getElementById("start100").addEventListener("click", carSet.init.bind(carSet, 100));
    document.getElementById("start1000").addEventListener("click", carSet.init.bind(carSet, 1000));
    document.getElementById("join").addEventListener("click", carSet.init.bind(carSet, 0));
    document.getElementById("pause").addEventListener("click", carSet.togglePause.bind(carSet));

    document.getElementById("addVehicle").addEventListener("click", function () {
        var count = document.getElementById("initVehicles").value;
        count = Math.max(1, count);
        carSet.ws.send("addVehicles|" + count);
    });
}

function initAll() {
    initMap();
    initButtons(carSet);
}


var carSet = {
    _cars: [],
    _carCount: 0,

    // start listening to the websocket and setup the callbacks
    init: function(count) {
        this.paused = false;

        if (this.ws)
            this.ws.close();

        if (this._cars) {
            this._cars.forEach(function (c) {
                c.remove();
            });
            this._cars = [];
            this._carCount = 0;
        }

        this.ws = new WebSocket("ws://localhost:8888");
        this.ws.binaryType = "arraybuffer";
        this.ws.onmessage = function (e) {
            this.processData(e.data);
            window.setTimeout(function () {
                // send the 'next' command to tell the server that it's ready to
                // received more data from the back end
                if (!this.paused)
                    this.ws.send("next");
            }.bind(this));
        }.bind(this);
        this.ws.onopen = function (e) {
            this.ws.send("start|" + count);
            this.ws.send("next");
        }.bind(this);
    },

    togglePause: function(e) {
        if (this.ws == null)
            return;
        if (this.paused) {
            this.ws.send("next");
            e.target.value = "Pause Simulation";
        } else {
            e.target.value = "Resume Simulation";
        }
        this.paused = !this.paused;
    },

    // deals with the set of data that comes from the websocket
    processData: function(data) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        var view = new DataView(data);
        var totalVel = 0;
        for (var i = 0; i < data.byteLength; i += 24) {
            var index = view.getInt32(i);
            var vel = view.getInt32(i + 4);
            var lat = view.getFloat64(i + 8);
            var lon = view.getFloat64(i + 16);
            totalVel += vel;
            if (this._cars[index]) {
                this._cars[index].moveTo(lat, lon, vel);
                this._cars[index].moved = true;
            } else {
                this._carCount++;
                this._cars[index] = new Car(index, vel, lat, lon);
            }
        }
        var liveVehicles = data.byteLength / 24;
        document.getElementById("info").innerHTML =
            "<p>Vehicles:" + liveVehicles + " (" + (this._carCount-liveVehicles)+"/"+this._carCount+")</p>" +
            "<p>Mean velocity: "+ Math.round(1000 * totalVel / liveVehicles) / 1000 +"</p>";
    }
};

L.AngleMarker = L.Marker.extend({
    options: {
        angle: 0
    },

    _setPos: function (pos) {
        //L.Marker.prototype._setPos.call(this, pos);
        ctx.translate(pos.x, pos.y);
        if (this.options.angle) {
            ctx.rotate(this.options.angle);
        }
        ctx.drawImage(icon, -icon.offsetWidth/2, -icon.offsetHeight/2);
        ctx.setTransform(1,0,0,1,0,0);
    }
});
L.angleMarker = function(latlngs, options) {
    return new L.AngleMarker(latlngs, options);
};

var Car = function(id, vel, lat, lon) {
    this.id = id;
    this.vel = vel;
    this.pos = [lat, lon];
    this.moveTo(lat, lon, vel);
};

Car.prototype = {

    moveTo: function(lat, lon, vel) {
        this.vel = vel;

        if (this.marker == null) {
            this.marker = L.circleMarker([lat,lon], {
                stroke: false,
                radius: 4,
                opacity: 0.8
                //icon: carIcon,
                //angle: 180*Math.random()
            });
            window.map.addLayer(this.marker);
        } else {
            //this.marker.stop();
            //this.marker.setLine(line);
            this.marker.setLatLng([lat, lon]);
        }
        this.marker.bindLabel(""+this.id + "|" + this.vel, {noHide: false});
        if (this.pos[0] != lat && this.pos[1] != lon)
            this.marker.options.angle = angleFromCoordinate(this.pos[0], this.pos[1], lat, lon);
        this.pos = [lat, lon];
        //this.marker.start();
    },

    remove: function() {
        if (this.marker)
            window.map.removeLayer(this.marker);
    }
}