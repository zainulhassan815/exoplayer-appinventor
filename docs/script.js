async function fetchData(url) {
  try {
    let response = await fetch(url);
    return await response.json();
  } catch (e) {
    console.e("Failed to fetch data : " + e);
  }
}

function copyTextToClipboard(text) {
  navigator.clipboard.writeText(text).then(function() {
    console.log('Async: Copying to clipboard was successful!');
  }, function(err) {
    console.error('Async: Could not copy text: ', err);
  });
}

async function initialize(path) {
  let functionsGrid = document.querySelector("#functions .docs-grid");
  let eventsGrid = document.querySelector("#events .docs-grid");
  let propertiesGrid = document.querySelector("#properties .docs-grid");
  let data = await fetchData(path + ".json");

  if (data !== null || data !== undefined) {
    // Add Function Cards
    let functionDocs = data["functions"];
    functionDocs.forEach((element) => {
      let card = createCard(
        element.title,
        element.image,
        element.description,
        element.params
      );
      functionsGrid.appendChild(card);
    });

    // Add Event Cards
    let eventDocs = data["events"];
    eventDocs.forEach((element) => {
      let card = createCard(
        element.title,
        element.image,
        element.description,
        element.params
      );
      eventsGrid.appendChild(card);
    });

    // Add Property Cards
    let propertyDocs = data["properties"];
    propertyDocs.forEach((element) => {
      let card = createCard(
        element.title,
        element.image,
        element.description,
        element.params
      );
      propertiesGrid.appendChild(card);
    });
  }
}

function createCard(title, image, description, params = []) {
  let card = document.createElement("div");
  card.setAttribute("class", "card");
  card.setAttribute("style", "position:relative;");
  let id = title.trim().replaceAll(" ", "");
  card.setAttribute("id", id);

  let paramsDivs = ``;
  params.forEach((value) => {
    paramsDivs += `<div class="params">
                  <span>${value.name} :</span> <span>${value.description}</span>
            </div>`;
  });

  const COPY_ICON = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-share-fill" viewBox="0 0 16 16">
  <path d="M11 2.5a2.5 2.5 0 1 1 .603 1.628l-6.718 3.12a2.499 2.499 0 0 1 0 1.504l6.718 3.12a2.5 2.5 0 1 1-.488.876l-6.718-3.12a2.5 2.5 0 1 1 0-3.256l6.718-3.12A2.5 2.5 0 0 1 11 2.5z"/>
  </svg>`;
  let copyBtn = document.createElement('button')
  copyBtn.setAttribute("class","copy-btn btn")
  copyBtn.innerHTML = COPY_ICON
  copyBtn.onclick = function() {
    // Copy current card's id and append it to the window url
    let url = `${window.location.href}#${id}`
    copyTextToClipboard(url)
  } 

  let template = `
  <img
      src=${image}
      alt=${title}
      class="card-img-top"
    />
    <div class="card-body">
      <h5 class="card-title">${title}</h5>
      <p class="card-text">
        ${description}
      </p>
      ${paramsDivs}
    </div>
  `;

  card.innerHTML = template;
  card.appendChild(copyBtn)
  return card;
}
