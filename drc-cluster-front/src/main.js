import Vue from 'vue'
import App from './App.vue'
import router from './router/router'
import './assets/style/index.less'
import './plugins/element'
import './plugins/server'
import './components/base'
import '@esign/el-ui-theme/lib/index.css'
import VueCodeHighlight from 'vue-code-highlight';
import 'vue-code-highlight/themes/prism-dark.css'
import G6 from '@antv/g6';

Vue.use(VueCodeHighlight)
Vue.use(G6)
Vue.config.productionTip = false

new Vue({
  router,
  render: h => h(App),
}).$mount('#app')
