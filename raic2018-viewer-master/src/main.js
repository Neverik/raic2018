let state = null;
let options = null;
let font;
let redrawn = 0;
let offset = [0, 0, 0];
let rot = [0, 0];
let pause = false;
let cache = [];
let step = 0;
let forward = true;

const socket = io.connect("localhost:8100");
socket.on("transfer", data => {
    receive(data);
});

function receive(d) {
    isState = d[0];
    data = d[1];
    if (isState) {
        console.log(data)
        cache.push(...data);
    } else {
        initialOptions = {
            field: {w: 200, l: 400},
            ball: {r: 20, color: {r: 0, g: 0, b: 0}},
            player: {r: 10},
            colors: [
                {r: 255, g: 0, b: 0},
                {r: 0, g: 0, b: 255}
            ],
            gate: {size: {w: 60, h: 40, d: 20}, color: {r: 0, g: 255, b: 0}}
        };
        options = mergeDeep(initialOptions, data);
    }
    loop();
}

function preload() {
    font = loadFont("res/font.otf");
}

function setup() {
    createCanvas(displayWidth, displayHeight, WEBGL);
}

function draw() {
    if (options == null) {
        background(250);
        if (font.font !== undefined) {
            const message = "Waiting for a client to connect...";
            const fontSize = 50;
            const messageWidth = message.split('').map(textWidth).reduce(function (x, y) { return x + y; });

            textFont(font);
            textAlign(LEFT, CENTER);
            textSize(fontSize);
            fill(55);
            text(message, -messageWidth / 2, 0, undefined, undefined);
            redrawn += 1;
            if (redrawn >= 2) {
                noLoop();
            }
        }
    } else {
        if (!pause) {
            if(forward) {
                if (cache.slice(step).length > 0 && pause === false) {
                    state = cache[step++];
                }
            } else {
                if (step > 0) {
                    state = cache[step--];
                }
            }
        }
        if (state == null) return;

        background(250);

        noStroke();
        ambientLight(128, 128, 128);
        this.directionalLight(128, 128, 128, 0, 0, -1);

        rotateX(PI / 2);
        ambientMaterial(200);
        
        translate(...offset);
        translate(0, 200, 0);
        rotateX(rot[1]);
        rotateY(rot[0] + PI / 2);

        scale(4);

        box(options.field.w, 0.1, options.field.l);

        push();
            translate(state.ball.x, state.ball.y, state.ball.z);
            sphere(options.ball.r);
        pop();

        state.teams.forEach((team, i) => {
            const color = options.colors[i];
            ambientMaterial(color.r, color.g, color.b);

            team.players.forEach(player => {
                push();
                translate(player.x, player.y, player.z);
                sphere(options.player.r);
                pop();
            })
        });

        push();
            ambientMaterial(options.gate.color.r, options.gate.color.g, options.gate.color.b);
            translate(0, options.gate.size.h / 2, options.field.l / 2 + options.gate.size.d / 2);
            box(options.gate.size.w, options.gate.size.h, options.gate.size.d);
            translate(0, 0, -options.field.l - options.gate.size.d);
            box(options.gate.size.w, options.gate.size.h, options.gate.size.d);
        pop();

        if (keyIsPressed) {
            if (keyCode == UP_ARROW) rot[1] -= 0.05;
            if (keyCode == DOWN_ARROW) rot[1] += 0.05;

            if (keyCode == RIGHT_ARROW) rot[0] += 0.1;
            if (keyCode == LEFT_ARROW) rot[0] -= 0.1;

            if (key === 'S' || key == 's') offset[1] -= 10;
            if (key === 'W' || key == 'w') offset[1] += 10;
        }
    }
}

function isObject(item) {
    return (item && typeof item === 'object' && !Array.isArray(item));
}

function mergeDeep(target, ...sources) {
    if (!sources.length) return target;
    const source = sources.shift();
  
    if (isObject(target) && isObject(source)) {
        for (const key in source) {
            if (isObject(source[key])) {
                if (!target[key]) Object.assign(target, { [key]: {} });
                    mergeDeep(target[key], source[key]);
            } else {
                Object.assign(target, { [key]: source[key] });
            }
        }
    }
  
    return mergeDeep(target, ...sources);
}

function keyPressed() {
    if (key === ' ') pause = !pause;
    if (key === 'Q' || key === 'q') {
        forward = false;
        pause = false;
    }
    if (key === 'E' || key === 'e') {
        forward = true;
        pause = false;
    }
    if (keyCode === ENTER) fullScreen();
}

function fullScreen(){
    if (canvas.RequestFullScreen){
        canvas.RequestFullScreen();
    } else if (canvas.webkitRequestFullScreen){
        canvas.webkitRequestFullScreen();
    } else if (canvas.mozRequestFullScreen){
        canvas.mozRequestFullScreen();
    } else if (canvas.msRequestFullscreen){
        canvas.msRequestFullscreen();
    } else {
        alert("This browser doesn't supporter fullscreen");
    }
}
