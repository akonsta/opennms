import { createRouter, createWebHashHistory } from 'vue-router'
import FileEditor from '@/containers/FileEditor.vue'
import Logs from '@/containers/Logs.vue'

const router = createRouter({
  history: createWebHashHistory('/opennms/ui'),
  routes: [
    {
      path: '/',
      name: 'nodes',
      component: () => import('@/containers/Nodes.vue')
    },
    {
      path: '/node/:id',
      name: 'Node Details',
      component: () => import('@/containers/NodeDetails.vue')
    },
    {
      path: '/inventory',
      name: 'Inventory',
      component: () => import('@/containers/Inventory.vue'),
      children: [
        {
          path: '',
          component: () => import('@/components/Inventory/StepAdd.vue')
        },
        {
          path: 'configure',
          component: () => import('@/components/Inventory/StepConfigure.vue')
        },
        {
          path: 'schedule',
          component: () => import('@/components/Inventory/StepSchedule.vue')
        }
      ]
    },
    {
      path: '/file-editor',
      name: 'FileEditor',
      component: FileEditor
    },
    {
      path: '/logs',
      name: 'Logs',
      component: Logs
    },
    {
      path: '/:pathMatch(.*)*', // catch other paths and redirect
      redirect: '/'
    }
  ]
})

export default router
