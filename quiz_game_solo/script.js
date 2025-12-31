const questionContainer = document.getElementById('question-container');
const questionText = document.getElementById('question-text');
const answerButtons = document.getElementById('answer-buttons');
const nextButton = document.getElementById('next-btn');
const resultContainer = document.getElementById('result-container');
const scoreElement = document.getElementById('score');
const totalQuestionsElement = document.getElementById('total-questions');

let currentQuestionIndex = 0;
let score = 0;

const questions = [
    {
        question: '¿Cuál es la función principal de una dirección IP?',
        answers: [
            { text: 'Identificar y localizar un dispositivo en una red', correct: true },
            { text: 'Aumentar la velocidad de internet', correct: false },
            { text: 'Bloquear virus', correct: false },
            { text: 'Encriptar datos', correct: false }
        ]
    },
    {
        question: '¿Cuál es el rol de una Puerta de Enlace (Gateway) predeterminada?',
        answers: [
            { text: 'Almacenar archivos de sitios web', correct: false },
            { text: 'Conectar una red local con otras redes (como Internet)', correct: true },
            { text: 'Asignar direcciones IP a los dispositivos', correct: false },
            { text: 'Traducir nombres de dominio a IPs', correct: false }
        ]
    },
    {
        question: '¿Qué hace un servidor DNS?',
        answers: [
            { text: 'Actúa como un firewall', correct: false },
            { text: 'Limpia el ordenador de malware', correct: false },
            { text: 'Traduce nombres de dominio (ej: google.com) a direcciones IP', correct: true },
            { text: 'Conecta físicamente tu ordenador al router', correct: false }
        ]
    },
    {
        question: '¿Cuál de las siguientes es una dirección IPv4 válida?',
        answers: [
            { text: '192.168.1.1', correct: true },
            { text: '00-1B-44-11-3A-B7', correct: false },
            { text: 'www.example.com', correct: false },
            { text: '2001:0db8:85a3:0000:0000:8a2e:0370:7334', correct: false }
        ]
    }
];

function startQuiz() {
    currentQuestionIndex = 0;
    score = 0;
    nextButton.classList.add('hide');
    resultContainer.classList.add('hide');
    totalQuestionsElement.innerText = questions.length;
    showQuestion(questions[currentQuestionIndex]);
}

function showQuestion(question) {
    resetState();
    questionText.innerText = question.question;
    question.answers.forEach(answer => {
        const button = document.createElement('button');
        button.innerText = answer.text;
        button.classList.add('btn');
        if (answer.correct) {
            button.dataset.correct = answer.correct;
        }
        button.addEventListener('click', selectAnswer);
        answerButtons.appendChild(button);
    });
}

function resetState() {
    nextButton.classList.add('hide');
    while (answerButtons.firstChild) {
        answerButtons.removeChild(answerButtons.firstChild);
    }
}

function selectAnswer(e) {
    const selectedButton = e.target;
    const correct = selectedButton.dataset.correct === 'true';
    if (correct) {
        score++;
    }
    Array.from(answerButtons.children).forEach(button => {
        setStatusClass(button, button.dataset.correct === 'true');
    });
    if (questions.length > currentQuestionIndex + 1) {
        nextButton.classList.remove('hide');
    } else {
        showResults();
    }
}

function setStatusClass(element, correct) {
    clearStatusClass(element);
    if (correct) {
        element.classList.add('correct');
    } else {
        element.classList.add('wrong');
    }
}

function clearStatusClass(element) {
    element.classList.remove('correct');
    element.classList.remove('wrong');
}

function showResults() {
    questionContainer.classList.add('hide');
    resultContainer.classList.remove('hide');
    scoreElement.innerText = score;
}

nextButton.addEventListener('click', () => {
    currentQuestionIndex++;
    showQuestion(questions[currentQuestionIndex]);
});

startQuiz();