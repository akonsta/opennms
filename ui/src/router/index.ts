import { createRouter, createWebHashHistory } from 'vue-router'
import Nodes from '@/containers/Nodes.vue'
import NodeDetails from '@/containers/NodeDetails.vue'
import Demo from '../components/Common/Demo/Demo.vue'

const router = createRouter({
  history: createWebHashHistory('/opennms/ui'),
  routes: [
    {
      path: '/',
      name: 'nodes',
      component: Nodes
    },
    {
      path: '/node/:id',
      name: 'Node Details',
      component: NodeDetails
    },
    {
      path: '/demo',
      name: 'Demo',
      component: Demo
    },
    {
      path: '/:pathMatch(.*)*', // catch other paths and redirect
      redirect: '/'
    }
  ]
})

export default router