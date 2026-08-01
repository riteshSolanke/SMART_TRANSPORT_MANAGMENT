import Swal from 'sweetalert2'

export function confirmAction({
  title,
  text,
  confirmText = 'Confirm',
  icon = 'warning',
  confirmColor = '#0d9488',
}) {
  return Swal.fire({
    title,
    text,
    icon,
    showCancelButton: true,
    confirmButtonText: confirmText,
    cancelButtonText: 'Keep it',
    reverseButtons: true,
    focusCancel: true,
    confirmButtonColor: confirmColor,
    cancelButtonColor: '#64748b',
    customClass: { popup: 'transport-alert' },
  })
}
