import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrdenList } from './orden-list';

describe('OrdenList', () => {
  let component: OrdenList;
  let fixture: ComponentFixture<OrdenList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrdenList],
    }).compileComponents();

    fixture = TestBed.createComponent(OrdenList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
