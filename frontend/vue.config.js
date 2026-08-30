module.exports = {
  outputDir: 'dist',
  publicPath: '/',
  devServer: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
};
