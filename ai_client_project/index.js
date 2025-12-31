const axios = require('axios');

const SERVER_URL = "http://192.168.1.158:1234/v1";

async function listModels() {
    try {
        const response = await axios.get(`${SERVER_URL}/models`);
        console.log("Available Models (Node.js):");
        response.data.data.forEach(model => {
            console.log(`- ${model.id}`);
        });
    } catch (error) {
        console.error(`Error connecting to AI server (Node.js): ${error.message}`);
    }
}

listModels();