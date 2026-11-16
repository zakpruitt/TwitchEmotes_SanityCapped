// The grid is rendered by Thymeleaf; this is search, play and copy-to-clipboard.
import {$} from "./api.js";

const grid = $("#grid");
const cards = [...grid.querySelectorAll(".sound")];
const count = $("#count");

const player = new Audio();
let playing = null;

function stop() {
    player.pause();
    if (playing) playing.innerHTML = "&#9654;";
    playing = null;
}

player.addEventListener("ended", stop);

$("#search").addEventListener("input", (event) => {
    const term = event.target.value.trim().toLowerCase();
    let shown = 0;
    for (const card of cards) {
        const matches = !term || card.dataset.search.toLowerCase().includes(term);
        card.hidden = !matches;
        shown += matches ? 1 : 0;
    }
    count.textContent = shown === cards.length
        ? `${cards.length} sounds`
        : `${shown} of ${cards.length} sounds`;
});

grid.addEventListener("click", async (event) => {
    const play = event.target.closest(".play");
    if (play) {
        const wasPlaying = playing === play;
        stop();
        if (wasPlaying) return;
        player.src = play.dataset.src;
        playing = play;
        play.innerHTML = "&#9632;";
        player.play().catch(stop);
        return;
    }

    const copy = event.target.closest(".copy");
    if (!copy) return;
    await navigator.clipboard.writeText(copy.closest(".sound").dataset.command);
    flash(copy.querySelector(".cmd"), "copied!");
});

document.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-copy]");
    if (!button) return;
    await navigator.clipboard.writeText($(button.dataset.copy).textContent.trim());
    flash(button, "Copied");
});

function flash(element, text) {
    const original = element.textContent;
    element.textContent = text;
    setTimeout(() => (element.textContent = original), 900);
}
