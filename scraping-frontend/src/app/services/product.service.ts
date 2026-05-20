import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Product {
  id: number;
  imageUrl: string;
  name: string;
  originalPrice: string;
  priceRon: number;
  exchangeRate: number;
  description: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProductService {

  private apiUrl = 'http://localhost:8081/api/products';
  private pdfUrl = 'http://localhost:8081/api/pdf/upload';

  constructor(private http: HttpClient) { }

  getProducts(search: string = '', sortBy: string = 'id', direction: string = 'asc'): Observable<Product[]> {
    let params = new HttpParams()
      .set('search', search)
      .set('sortBy', sortBy)
      .set('direction', direction);

    return this.http.get<Product[]>(this.apiUrl, { params });
  }

  updateProduct(id: number, product: Product): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${id}`, product);
  }

  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  uploadPdf(file: File): Observable<Blob> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(this.pdfUrl, formData, { responseType: 'blob' });
  }
}