import {
  FiBarChart2,
  FiCreditCard,
  FiGrid,
  FiMap,
  FiTag,
  FiTruck,
  FiUser,
  FiUsers,
} from 'react-icons/fi'

export const navigationItems = [
  {
    label: 'Overview',
    path: '/dashboard',
    icon: FiGrid,
    roles: ['PASSENGER', 'CONDUCTOR', 'DISPATCHER', 'TRANSPORT_MANAGER', 'ADMIN'],
  },
  {
    label: 'Routes',
    path: '/routes',
    icon: FiMap,
    roles: ['PASSENGER', 'CONDUCTOR', 'DISPATCHER', 'TRANSPORT_MANAGER', 'ADMIN'],
  },
  {
    label: 'My tickets',
    path: '/tickets',
    icon: FiTag,
    roles: ['PASSENGER', 'CONDUCTOR', 'TRANSPORT_MANAGER', 'ADMIN'],
  },
  {
    label: 'Payments',
    path: '/payments',
    icon: FiCreditCard,
    roles: ['PASSENGER', 'CONDUCTOR', 'ADMIN'],
  },
  {
    label: 'Fleet',
    path: '/fleet',
    icon: FiTruck,
    roles: ['CONDUCTOR', 'DISPATCHER', 'TRANSPORT_MANAGER', 'ADMIN'],
  },
  {
    label: 'Analytics',
    path: '/analytics',
    icon: FiBarChart2,
    roles: ['TRANSPORT_MANAGER', 'ADMIN'],
  },
  {
    label: 'Users',
    path: '/users',
    icon: FiUsers,
    roles: ['ADMIN'],
  },
  {
    label: 'Profile',
    path: '/profile',
    icon: FiUser,
    roles: ['PASSENGER', 'CONDUCTOR', 'DISPATCHER', 'TRANSPORT_MANAGER', 'ADMIN'],
  },
]

export function navigationForRole(role) {
  return navigationItems.filter((item) => item.roles.includes(role))
}
