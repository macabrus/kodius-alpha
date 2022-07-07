function debounce(func, timeout = 300){
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => func.apply(this, args), timeout);
    };
}

const ws = new WebSocket('ws://localhost:7000/compute-price')
ws.onmessage = function (event) {
    const result = JSON.parse(event.data);
    console.log("Message received...", result);
    document.querySelector('span#total-price').textContent = `$${result.discounted / 100}`;
};

async function updatePrice(e) {
    console.log('Something changed in form!')
    let form = [...document.querySelectorAll('input, select')]
        .map(input => {
            let key = input.getAttribute('name');
            let val;
            if (input.getAttribute('type') === 'checkbox') {
                val = input.checked;
            }
            else {
                val = input.value.trim() || null;
            }
            return { [key]: val }
        })
        .reduce((acc, x) => ({...acc, ...x}));
    console.log(form);
    ws.send(JSON.stringify(form));
}