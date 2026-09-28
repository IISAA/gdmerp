import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Clonamos la petición original y le agregamos un header personalizado.
  // Usamos 'X-GDM-App-Version' para evitar que un header de 'Authorization' 
  // falso haga que tu Spring Boot rechace la petición (si no tiene seguridad configurada).
  const modifiedReq = req.clone({
    setHeaders: {
      'X-GDM-App-Version': '1.0.0',
      'X-Requested-With': 'Angular22-Standalone'
    }
  });

  // Pasamos la petición modificada al siguiente manejador
  return next(modifiedReq);
};