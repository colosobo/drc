/* eslint-disable */
const path = require('path')
const packageConfig = require('./package.json')

// 版本号取package.json
const APP_VERSION = packageConfig.version
const APP_NAME = packageConfig.name

// 应用部署的CDN目录
const CDN_PATH = `//asset.tsign.cn/apps/${APP_NAME}/`

// 生产环境
const IS_PRODUCTION_ENV = process.env.NODE_ENV === 'production'

// 编译模式：dev test sml prod
const { MODE } = process.env

// 需要部署在nginx下的模式 不变的文件放到ngxin 变化的文件单独打包放到oss
const NGINX_MODE = ['test', 'sml', 'prod', 'office']
const IS_NGINX_MODE = NGINX_MODE.includes(MODE)

// 打包目录
const OUTPUT_DIR = 'dist'

// resolve方法
function resolve(dir) {
  return path.join(__dirname, '.', dir)
}

// console.log('MODE', MODE)
// 获取参数
const getArgv = () => {
  if (MODE !== 'dev') return ''
  const findIndex = process.argv.findIndex(v => v === '--groupId')
  if (findIndex === -1) return ''
  const groupId = process.argv[findIndex + 1]

  return groupId || ''
}

module.exports = {
  outputDir: OUTPUT_DIR,
  publicPath: IS_NGINX_MODE ? `${CDN_PATH}` : '/',
  assetsDir: IS_NGINX_MODE ? `${MODE}/${APP_VERSION}` : 'static',
  indexPath: IS_NGINX_MODE ? 'nginx/index.html' : 'index.html',
  crossorigin: 'anonymous',
  lintOnSave: false,
  chainWebpack: config => {
    config.optimization.minimizer('terser').tap(args => {
      args[0].terserOptions.compress.drop_console = true
      args[0].terserOptions.compress.drop_debugger = true

      return args
    })

    // 修改CopyWebpackPlugin
    config.plugin('copy').tap(args => {
      args[0][0].to = IS_NGINX_MODE ? path.resolve(__dirname, `./${OUTPUT_DIR}/nginx`) : path.resolve(__dirname, `./${OUTPUT_DIR}`)

      return args
    })

    // 优化
    config.plugins.delete('prefetch')
    config.plugins.delete('preload')

    // 修改 process.env
    config.plugin('define').tap(definitions => {
      Object.assign(definitions[0]['process.env'], {
        groupId: JSON.stringify(getArgv()),
      })

      return definitions
    })
  },
}
