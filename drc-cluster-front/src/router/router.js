import Vue from 'vue'
import VueRouter from 'vue-router'
import Layout from '../views/Base'
import SubLayout from '@/components/layout/SubLayout'

const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => err)
}

Vue.use(VueRouter)

const router = new VueRouter({
  base: 'drc-cluster-front',
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'Base',
      redirect: '/index',
    },
    {
      path: '/index',
      name: 'Index',
      redirect: '/index',
      component: Layout,
      meta: { title: '分布式 DRC ' },
      children: [
        {
          path: '',
          name: '',
          component: () => import('../views/index/Index'),
        },
      ],
    },
    {
      path: '/task',
      name: 'Task',
      redirect: '/task',
      component: Layout,
      meta: { title: '任务列表' },
      children: [
        {
          path: '',
          name: '',
          component: () => import('../views/task/Task'),
        },
        {
          path: '/task/:taskId',
          name: 'SubTask',
          component: () => import('../views/task/SubTask'),
          meta: { title: '子任务' },
        },
      ],
    },
    {
      path: '/add',
      name: 'Add',
      redirect: '/add',
      component: Layout,
      meta: { title: '新增' },
      children: [
        {
          path: '',
          name: '',
          component: () => import('../views/add/Add'),
        },
      ],
    },
    // {
    //   path: '/jar-task',
    //   name: 'Jartask',
    //   redirect: '/jar-task',
    //   component: Layout,
    //   meta: { title: 'Jar包任务' },
    //   children: [
    //     {
    //       path: '',
    //       name: '',
    //       component: () => import('../views/Jar'),
    //     },
    //   ],
    // },
  ],
})

export default router
