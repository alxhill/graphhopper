/**
 * Created by alexander on 22/02/2016.
 */

var carIcon = L.icon({
    iconUrl: 'car-icon.png',
    iconSize: [40,30]
});

var map = L.map('map').setView([51.505, -0.09], 13);
L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18,
    id: 'alxhill.p69lilm1',
    accessToken: 'pk.eyJ1IjoiYWx4aGlsbCIsImEiOiJjaWtyMnM5cTAwMDFzd2RrcWxjdW14dGlhIn0._vGArimDzlTVhET5T_GZzA'
}).addTo(map);

var line = L.polyline([[51.505, -0.09], [51.5, -0.08]]);
var marker1 = L.animatedMarker(line.getLatLngs(), {icon: carIcon});
console.log(line.getLatLngs(), marker1);
map.addLayer(marker1);
