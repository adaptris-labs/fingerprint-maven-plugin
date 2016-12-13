ko.components.register('component', {
    viewModel: { fromUrl: 'component.js', constructorName: "constructorName", maxCacheAge: 600000 },
    template: { fromUrl: "component.tmpl.html", maxCacheAge: 600000 },
    synchronous: true
});

var differentPatternUrl = { differentPatternUrl: 'component.js' };
