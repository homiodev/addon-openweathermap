class OpenWeatherWidget extends HTMLElement {

    setContext(widget) {
        widget.dataWarehouse.subscribe(data => {
            if(data) {
                this.weather = data;
                this.render();
            }
        });
    }

    render() {
        const formatTime = (timestamp) => {
            const date = new Date(timestamp * 1000);
            return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
        };
        const formatDate = (timestamp) => {
            const date = new Date(timestamp * 1000);
            return date.toLocaleTimeString([], { hour: '2-digit', hour12: false });
        };

        if(this.selectedDayIdx != null) {
            const day = this.weather.forecast[this.selectedDayIdx];
            const hoursArray = Object.values(day.hours).sort((a, b) => a.dt - b.dt);
            this.content.innerHTML = `                    
                <div class="weather-widget forecast hours">
                <button class="back-btn">← Back(${day.name})</button> 
                ${hoursArray.map(hour => ` 
                    <div class="day">
                    <div class="name">${formatDate(hour.dt / 1000)}</div>
                    <img class="icon" src="https://openweathermap.org/img/wn/${hour.icon}.png" />
                    <div class="min">${Math.round(hour.temperature)}°<b>C</b></div>
                    </div>
                `).join('')}
                </div>
            `;
            setTimeout(()=> {
                this.content.querySelector('.back-btn').addEventListener('click', () => {
                    this.selectedDayIdx = null;
                    this.render();
                });
            }, 100);
            return;
        }

        this.content.innerHTML = `
            <div class="weather-widget">
              <h2 class="city-name">${this.weather.city} - ${this.weather.condition}</h2>
              <div class="current-weather">
                <div class="temperature">
                  <div class="main">
                    <img class="icon" src="https://openweathermap.org/img/wn/${this.weather.icon}.png" />
                    ${Math.round(this.weather.temperature)}°C
                  </div>
                  <div class="details">
                    <div class="block">
                    <div class="info">
                      <i class="fas fa-tachometer-alt"></i>
                      <div class="value">${this.weather.pressure} hPa</div>
                    </div>
                    <div class="info">
                      <i class="fas fa-tint"></i>
                      <div class="value">${this.weather.humidity}%</div>
                    </div>
                    </div>
                    <div class="block">
                    <div class="info">
                      <i class="fas fa-wind"></i>
                      <div class="value">${this.weather.windSpeed} m/s</div>
                    </div>
                    <div class="info">
                      <i class="fas fa-eye"></i>
                      <div class="value">${this.weather.visibility / 1000} km</div>
                    </div>
                    </div>
                    <div class="block">
                    <div class="info">
                      <i class="fas fa-sun"></i>
                      <div class="value">${formatTime(this.weather.sunrise)}</div>
                    </div>
                    <div class="info">
                      <i class="fas fa-moon"></i>
                      <div class="value">${formatTime(this.weather.sunset)}</div>
                    </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="forecast clickable">
                ${this.weather.forecast.map(day => `
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
                  <span>${Math.round(this.weather.temperature)}<b>°C</b></span>
                </div>                          
                <div>
                  <i class="fas fa-fw fa-tint"></i>
                  <span>${this.weather.humidity}<b>%</b></span>
                </div>                                                
                  <div>
                    <i class="fas fa-fw fa-wind"></i>
                    <span>${this.weather.windSpeed}<b>m/s</b></span>
                  </div>
              </div>
            </div>
        `;

        setTimeout(()=> {
            this.content.querySelectorAll('.forecast .day').forEach((dayElem, idx) => {
                dayElem.addEventListener('click', () => {
                    this.selectedDayIdx = idx;
                    this.render();
                });
            });
        }, 100)
    }
}

customElements.define("openweather-widget", OpenWeatherWidget);