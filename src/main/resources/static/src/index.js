import render from 'd3-render';

function draw(data) {

  var shapes = []
  const elementWidth = 5;
  const spacing = 2;
//console.log(data)
  for (var key in data) {
    const sectionElements = data[key]
    const randomColor = Math.floor(Math.random()*16777215).toString(16);
    const color = "#" + randomColor;
    sectionElements.forEach((element) => {
        console.log(element)
        shapes.push({ append: 'rect', fill: color, x: element.x * (elementWidth + spacing) , y: element.y * (elementWidth + spacing), width: elementWidth, height: elementWidth })
        shapes.push({ append: 'rect', fill: color, x: element.x * (elementWidth + spacing) , y: element.y * (elementWidth + spacing), width: elementWidth, height: elementWidth })
     })
  }
  console.log(shapes)
//  const shapes = [
//    { append: 'rect', fill: 'blue', x: 0, y: 0, width: 20, height: 20 },
//    { append: 'rect', fill: 'blue', x: 22, y: 0, width: 20, height: 20 },
//    { append: 'rect', fill: 'blue', x: 0, y: 22, width: 20, height: 20 },
//    { append: 'rect', fill: 'blue', x: 22, y: 22, width: 20, height: 20 },
//  ];

  // Call render again
  render('#root', shapes);
  console.log('boom boom')
}

setTimeout(() => {
  // Set some updated data

}, 20);

fetch('http://localhost:8081/api/v1/sections')
    .then(res => res.json())
    .then((out) => {
        draw(out)
}).catch(
    err => console.log(err)
);
