/**
 * Premium 3-State Theme Slider System
 * Supporting: Light, Device (System), and Dark modes.
 */
(function () {
  const htmlEl = document.documentElement;
  const THEME_COLORS = { light: '#f7f8fa', dark: '#12141a' };

  const savedTheme = localStorage.getItem('theme') || 'device';

  function getResolvedTheme(theme) {
    if (theme === 'dark') return 'dark';
    if (theme === 'light') return 'light';
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }

  function updateThemeColorMeta(resolved) {
    let meta = document.querySelector('meta[name="theme-color"]:not([media])');
    if (!meta) {
      meta = document.createElement('meta');
      meta.name = 'theme-color';
      document.head.appendChild(meta);
    }
    meta.content = THEME_COLORS[resolved];
  }

  function applyTheme(theme) {
    htmlEl.setAttribute('data-active-theme', theme);

    if (theme === 'dark') {
      htmlEl.classList.remove('theme-light');
      htmlEl.classList.add('theme-dark');
    } else if (theme === 'light') {
      htmlEl.classList.remove('theme-dark');
      htmlEl.classList.add('theme-light');
    } else {
      htmlEl.classList.remove('theme-light', 'theme-dark');
    }

    const resolved = getResolvedTheme(theme);
    htmlEl.setAttribute('data-resolved-theme', resolved);
    updateThemeColorMeta(resolved);
  }

  applyTheme(savedTheme);

  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    const current = localStorage.getItem('theme') || 'device';
    if (current === 'device') {
      applyTheme('device');
    }
  });

  function initSlider() {
    const placeholders = [
      document.getElementById('theme-toggle'),
      document.getElementById('theme-toggle-footer'),
    ].filter((el) => el !== null);

    if (placeholders.length === 0) return;

    placeholders.forEach((placeholder) => {
      const sliderContainer = document.createElement('div');
      sliderContainer.className = 'theme-slider';
      if (placeholder.id === 'theme-toggle-footer') {
        sliderContainer.id = 'theme-toggle-footer';
      }
      sliderContainer.setAttribute('role', 'radiogroup');
      sliderContainer.setAttribute('aria-label', 'Theme selection');

      sliderContainer.innerHTML = `
                <div class="theme-slider-track">
                    <div class="theme-slider-thumb"></div>
                    <button class="theme-slider-btn" data-theme="light" role="radio" aria-checked="false" aria-label="Light mode" title="Light Theme">
                        <svg class="slider-icon sun-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="12" r="5"/>
                            <line x1="12" y1="1" x2="12" y2="3"/>
                            <line x1="12" y1="21" x2="12" y2="23"/>
                            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
                            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
                            <line x1="1" y1="12" x2="3" y2="12"/>
                            <line x1="21" y1="12" x2="23" y2="12"/>
                            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
                            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
                        </svg>
                    </button>
                    <button class="theme-slider-btn" data-theme="device" role="radio" aria-checked="false" aria-label="System mode" title="Follow System Theme">
                        <svg class="slider-icon monitor-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
                            <line x1="8" y1="21" x2="16" y2="21"/>
                            <line x1="12" y1="17" x2="12" y2="21"/>
                        </svg>
                    </button>
                    <button class="theme-slider-btn" data-theme="dark" role="radio" aria-checked="false" aria-label="Dark mode" title="Dark Theme">
                        <svg class="slider-icon moon-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
                        </svg>
                    </button>
                </div>
            `;

      placeholder.parentNode.replaceChild(sliderContainer, placeholder);
    });

    function updateAllSlidersUI(activeTheme) {
      const allSliders = document.querySelectorAll('.theme-slider');
      allSliders.forEach((slider) => {
        const buttons = slider.querySelectorAll('.theme-slider-btn');
        buttons.forEach((btn) => {
          const isSelected = btn.getAttribute('data-theme') === activeTheme;
          btn.setAttribute('aria-checked', isSelected ? 'true' : 'false');
        });
      });
    }

    const currentTheme = localStorage.getItem('theme') || 'device';
    updateAllSlidersUI(currentTheme);

    const allSliders = document.querySelectorAll('.theme-slider');
    allSliders.forEach((slider) => {
      const buttons = slider.querySelectorAll('.theme-slider-btn');
      buttons.forEach((btn) => {
        btn.addEventListener('click', () => {
          const theme = btn.getAttribute('data-theme');
          if (theme === 'device') {
            localStorage.removeItem('theme');
          } else {
            localStorage.setItem('theme', theme);
          }
          applyTheme(theme);
          updateAllSlidersUI(theme);
        });
      });
    });

    const currentYearElements = document.querySelectorAll('.current-year');
    currentYearElements.forEach((el) => {
      el.textContent = new Date().getFullYear();
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initSlider);
  } else {
    initSlider();
  }

  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  window.addEventListener('pageshow', () => {
    document.body.classList.add('page-loaded');
  });

  window.addEventListener('load', () => {
    if (prefersReducedMotion) return;

    document.querySelectorAll('a').forEach((link) => {
      const href = link.getAttribute('href');
      if (!href) return;

      const isLocalLink =
        link.hostname === window.location.hostname &&
        !link.hash &&
        link.target !== '_blank' &&
        !href.startsWith('mailto:') &&
        !href.startsWith('tel:') &&
        !href.startsWith('javascript:');

      if (isLocalLink) {
        link.addEventListener('click', (e) => {
          e.preventDefault();
          document.body.classList.remove('page-loaded');
          setTimeout(() => {
            window.location.href = href;
          }, 220);
        });
      }
    });
  });
})();
