class OpenWeatherWidget extends HTMLElement {
    constructor() {
        super();
        this.attachShadow({mode: "open"});
    }

    setContext(widget) {
        widget.dataWarehouse.subscribe(info => {
            this.info = info;
            this.render();
        });
    }

    render() {
        if (!this.info) return;
        this.content = this.shadowRoot.querySelector(".custom-widget-panel");

        const weather = this.info;

        // Helper function to format time (convert Unix timestamp to HH:MM)
        const formatTime = (timestamp) => {
            const date = new Date(timestamp * 1000);
            return date.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});
        };

        this.content.innerHTML = `
            <div class="weather-widget">
              <h2 class="city-name">${weather.city} - ${weather.condition}</h2>
              <div class="current-weather">
                <div class="temperature">
                  <div class="main">
                    <img class="icon" src="https://openweathermap.org/img/wn/${weather.icon}.png" />
                    ${Math.round(weather.temperature)}°C
                  </div>
                  <div class="details">
                    <div class="block">
                    <div class="info">
                      <i class="fas fa-tachometer-alt"></i>
                      <div class="value">${weather.pressure} hPa</div>
                    </div>
                    <div class="info">
                      <i class="fas fa-tint"></i>
                      <div class="value">${weather.humidity}%</div>
                    </div>
                    </div>
                    <div class="block">
                    <div class="info">
                      <i class="fas fa-wind"></i>
                      <div class="value">${weather.windSpeed} m/s</div>
                    </div>
                    <div class="info">
                      <i class="fas fa-eye"></i>
                      <div class="value">${weather.visibility / 1000} km</div>
                    </div>
                    </div>
                    <div class="block">
                    <div class="info">
                      <i class="fas fa-sun"></i>
                      <div class="value">${formatTime(weather.sunrise)}</div>
                    </div>
                    <div class="info">
                      <i class="fas fa-moon"></i>
                      <div class="value">${formatTime(weather.sunset)}</div>
                    </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="forecast">
                ${weather.forecast.map(day => `
                  <div class="day">
                    <div class="name">${day.name}</div>
                    <img class="icon" src="https://openweathermap.org/img/wn/${day.icon}.png" />
                    <div class="max">${Math.round(day.maxTemp)}<b>°C</b></div>
                    <div class="min">${Math.round(day.minTemp)}<b>°C</b></div>
                  </div>
                `).join('')}
              </div>
              <div class="footer"> 
                <div>
                  <i class="fas fa-fw fa-temperature-high"></i>
                  <span>${Math.round(weather.temperature)}<b>°C</b></span>
                </div>                          
                <div>
                  <i class="fas fa-fw fa-tint"></i>
                  <span>${weather.humidity}<b>%</b></span>
                </div>                                                
                  <div>
                    <i class="fas fa-fw fa-wind"></i>
                    <span>${weather.windSpeed}<b>m/s</b></span>
                  </div>
              </div>
            </div>
        `;
    }
}

customElements.define("openweather-widget", OpenWeatherWidget);