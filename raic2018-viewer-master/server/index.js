const static = require('node-static');
const files = new static.Server("../", { cache: -1 });

var url = 'http://localhost:8080';
var start = (process.platform == 'darwin'? 'open': process.platform == 'win32'? 'start': 'xdg-open');
require('child_process').exec(start + ' ' + url);

const server = require('http').createServer(function (request, response) {
    request.addListener('end', function () {
        files.serve(request, response);
    }).resume();
});
/*
const io = require("socket.io")(server);
io.on("connection", client => {
    // example
    client.emit("transfer", [false, {}]);
    let states = [];
    for (let i = 0; i < 200; i++) {
        states.push({
            ball: {x: 20, y: 50 + i, z: 30},
            teams: [
                {players: [
                    {x: 10 - i, y: 2, z: 2},
                    {x: -20 + i, y: 2, z: 17}]},
                {players: [
                    {x: 17, y: 2, z: 50 + i},
                    {x: -30, y: 2, z: -50 - i}]}]});
    }
    client.emit("transfer", [true, states])
});
*/
server.listen(8080);
