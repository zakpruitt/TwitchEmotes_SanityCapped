// The grid is rendered by Thymeleaf; this is search and copy-to-clipboard.
import { $ } from "./api.js";

const grid = $("#grid");
const cards = [...grid.querySelectorAll(".emote")];
const count = $("#count");

$("#search").addEventListener("input", (event) => {
  const term = event.target.value.trim().toLowerCase();
  let shown = 0;
  for (const card of cards) {
    const matches = !term || card.dataset.search.toLowerCase().includes(term);
    card.hidden = !matches;
    shown += matches ? 1 : 0;
  }
  count.textContent = shown === cards.length
    ? `${cards.length} emotes`
    : `${shown} of ${cards.length} emotes`;
});

grid.addEventListener("click", async (event) => {
  const card = event.target.closest(".emote");
  if (!card) return;
  await navigator.clipboard.writeText(card.dataset.name);
  flash(card.querySelector(".name"), "copied!");
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
